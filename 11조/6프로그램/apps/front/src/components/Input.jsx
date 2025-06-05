import "./Input.css";

const Input = ({
  placeholder,
  type,
  name,
  value,
  onChange,
  className,
  disabled,
}) => {
  return (
    <div className="input-wrapper">
      <input
        type={type}
        className={`input-field ${className || ""}`}
        placeholder={placeholder}
        name={name}
        value={value || ""}
        onChange={onChange}
        disabled={disabled}
      />
    </div>
  );
};

export default Input;
